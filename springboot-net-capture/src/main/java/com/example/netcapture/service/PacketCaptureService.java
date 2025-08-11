package com.example.netcapture.service;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.pcap4j.core.BpfProgram.BpfCompileMode;
import org.pcap4j.core.NotOpenException;
import org.pcap4j.core.PacketListener;
import org.pcap4j.core.PcapHandle;
import org.pcap4j.core.PcapNativeException;
import org.pcap4j.core.PcapNetworkInterface;
import org.pcap4j.core.Pcaps;
import org.pcap4j.packet.EthernetPacket;
import org.pcap4j.packet.IpV4Packet;
import org.pcap4j.packet.Packet;
import org.pcap4j.packet.TcpPacket;
import org.pcap4j.packet.UdpPacket;
import org.pcap4j.util.NifSelector;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import com.example.netcapture.config.NetworkCaptureProperties;
import com.example.netcapture.entity.PacketInfo;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class PacketCaptureService {
    
    private final NetworkCaptureProperties captureProperties;
    private final PacketAnalysisService analysisService;
    private final PacketWebSocketHandler webSocketHandler;
    private final ThreadPoolTaskExecutor taskExecutor;
    
    public PacketCaptureService(NetworkCaptureProperties captureProperties,
                               PacketAnalysisService analysisService,
                               PacketWebSocketHandler webSocketHandler,
                               @Qualifier("captureTaskExecutor") ThreadPoolTaskExecutor taskExecutor) {
        this.captureProperties = captureProperties;
        this.analysisService = analysisService;
        this.webSocketHandler = webSocketHandler;
        this.taskExecutor = taskExecutor;
    }
    
    private PcapHandle handle;
    private PcapNetworkInterface selectedNetworkInterface;
    private final AtomicBoolean isCapturing = new AtomicBoolean(false);
    private final AtomicLong capturedPackets = new AtomicLong(0);
    
    public List<PcapNetworkInterface> getAvailableNetworkInterfaces() throws PcapNativeException {
        try {
            return Pcaps.findAllDevs();
        } catch (Exception e) {
            log.error("获取网络接口失败，可能是缺少 WinPcap/Npcap 库: {}", e.getMessage());
            throw new PcapNativeException("请确保已安装 Npcap: https://npcap.com/", e);
        }
    }
    
    public void startCapture() throws PcapNativeException, NotOpenException {
        startCapture(captureProperties.getNetworkInterface(), captureProperties.getFilter());
    }
    
    public void startCapture(String interfaceName, String filter) throws PcapNativeException, NotOpenException {
        if (isCapturing.get()) {
            throw new IllegalStateException("Packet capture is already running");
        }
        
        selectedNetworkInterface = selectNetworkInterface(interfaceName);
        
        handle = selectedNetworkInterface.openLive(
            captureProperties.getBufferSize(),
            captureProperties.isPromiscuous() ? 
                PcapNetworkInterface.PromiscuousMode.PROMISCUOUS : 
                PcapNetworkInterface.PromiscuousMode.NONPROMISCUOUS,
            captureProperties.getTimeout()
        );
        
        if (filter != null && !filter.trim().isEmpty()) {
            handle.setFilter(filter, BpfCompileMode.OPTIMIZE);
        }
        
        PacketListener listener = packet -> {
            try {
                PacketInfo packetInfo = parsePacket(packet);
                if (packetInfo != null) {
                    analysisService.analyzePacket(packetInfo);
                    webSocketHandler.broadcastPacket(packetInfo);
                    capturedPackets.incrementAndGet();
                    
                    if (captureProperties.getMaxPackets() > 0 && 
                        capturedPackets.get() >= captureProperties.getMaxPackets()) {
                        stopCapture();
                    }
                }
            } catch (Exception e) {
                log.error("Error processing packet", e);
            }
        };
        
        isCapturing.set(true);
        capturedPackets.set(0);
        
        taskExecutor.execute(() -> {
            try {
                log.info("Starting packet capture on interface: {}", selectedNetworkInterface.getName());
                handle.loop(-1, listener);
            } catch (Exception e) {
                log.error("Packet capture error", e);
            } finally {
                isCapturing.set(false);
            }
        });
    }
    
    public void stopCapture() {
        if (!isCapturing.get()) {
            return;
        }
        
        try {
            if (handle != null && handle.isOpen()) {
                handle.breakLoop();
                handle.close();
            }
            isCapturing.set(false);
            log.info("Packet capture stopped. Total packets captured: {}", capturedPackets.get());
        } catch (Exception e) {
            log.error("Error stopping packet capture", e);
        }
    }
    
    public boolean isCapturing() {
        return isCapturing.get();
    }
    
    public long getCapturedPacketsCount() {
        return capturedPackets.get();
    }
    
    private PcapNetworkInterface selectNetworkInterface(String interfaceName) throws PcapNativeException {
        List<PcapNetworkInterface> allDevs = Pcaps.findAllDevs();
        
        if (interfaceName != null && !interfaceName.trim().isEmpty()) {
            return allDevs.stream()
                .filter(nif -> nif.getName().equals(interfaceName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Network interface not found: " + interfaceName));
        }
        
        return allDevs.stream()
            .filter(nif -> {
                try {
                    return nif.getAddresses().stream()
                        .anyMatch(addr -> {
                            InetAddress inetAddr = addr.getAddress();
                            return inetAddr != null && 
                                   !inetAddr.isLoopbackAddress() && 
                                   !inetAddr.isLinkLocalAddress();
                        });
                } catch (Exception e) {
                    return false;
                }
            })
            .findFirst()
            .orElse(allDevs.get(0));
    }
    
    private PacketInfo parsePacket(Packet packet) {
        try {
            PacketInfo info = new PacketInfo();
            info.setCaptureTime(LocalDateTime.now());
            info.setPacketLength(packet.length());
            info.setNetworkInterface(selectedNetworkInterface.getName());
            
            EthernetPacket ethernetPacket = packet.get(EthernetPacket.class);
            if (ethernetPacket == null) {
                return null;
            }
            
            IpV4Packet ipPacket = ethernetPacket.getPayload().get(IpV4Packet.class);
            if (ipPacket != null) {
                info.setSourceIp(ipPacket.getHeader().getSrcAddr().getHostAddress());
                info.setDestinationIp(ipPacket.getHeader().getDstAddr().getHostAddress());
                
                TcpPacket tcpPacket = ipPacket.getPayload().get(TcpPacket.class);
                if (tcpPacket != null) {
                    info.setProtocol("TCP");
                    info.setSourcePort(tcpPacket.getHeader().getSrcPort().valueAsInt());
                    info.setDestinationPort(tcpPacket.getHeader().getDstPort().valueAsInt());
                    info.setTcpSeqNumber(Long.parseLong(tcpPacket.getHeader().getSequenceNumber() + ""));
                    info.setTcpAckNumber(Long.parseLong(tcpPacket.getHeader().getAcknowledgmentNumber() + ""));
                    info.setTcpFlags(tcpPacket.getHeader().toString());
                    
                    byte[] payload = tcpPacket.getPayload() != null ? 
                        tcpPacket.getPayload().getRawData() : null;
                    if (payload != null && payload.length > 0) {
                        // 先截取字节数组，再转换为字符串，避免字符编码问题
                        int maxLength = Math.min(payload.length, 1000);
                        byte[] truncatedPayload = new byte[maxLength];
                        System.arraycopy(payload, 0, truncatedPayload, 0, maxLength);
                        
                        try {
                            String payloadStr = new String(truncatedPayload, "UTF-8");
                            info.setPayload(payloadStr);
                        } catch (Exception e) {
                            // 如果UTF-8解码失败，使用默认编码
                            String payloadStr = new String(truncatedPayload);
                            info.setPayload(payloadStr);
                        }
                    }
                } else {
                    UdpPacket udpPacket = ipPacket.getPayload().get(UdpPacket.class);
                    if (udpPacket != null) {
                        info.setProtocol("UDP");
                        info.setSourcePort(udpPacket.getHeader().getSrcPort().valueAsInt());
                        info.setDestinationPort(udpPacket.getHeader().getDstPort().valueAsInt());
                    } else {
                        info.setProtocol("OTHER");
                    }
                }
            } else {
                // 如果没有IPv4包，也设置一个协议类型
                info.setProtocol("UNKNOWN");
            }
            
            return info;
        } catch (Exception e) {
            log.debug("Error parsing packet", e);
            return null;
        }
    }
}
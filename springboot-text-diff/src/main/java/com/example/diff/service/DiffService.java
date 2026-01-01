package com.example.diff.service;

import com.example.diff.model.DiffLine;
import com.example.diff.model.DiffResult;
import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.DeltaType;
import com.github.difflib.patch.Patch;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class DiffService {

    /**
     * 比对两个配置文本的差异，返回 Git 风格的左右对比结果
     */
    public DiffResult compareConfigs(String original, String revised) {
        List<String> originalLines = Arrays.asList(original.split("\\r?\\n"));
        List<String> revisedLines = Arrays.asList(revised.split("\\r?\\n"));

        Patch<String> patch = DiffUtils.diff(originalLines, revisedLines);

        DiffResult result = new DiffResult();
        result.setHasChanges(!patch.getDeltas().isEmpty());

        // 构建 Git 风格的行级对比
        List<DiffLine> diffLines = buildGitStyleDiff(originalLines, revisedLines, patch);
        result.setDiffLines(diffLines);

        // 同时保留原有的 change 信息（用于其他用途）
        for (AbstractDelta<String> delta : patch.getDeltas()) {
            com.example.diff.model.DiffChange change = new com.example.diff.model.DiffChange();
            change.setType(delta.getType().name());
            change.setSourceLine(delta.getSource().getPosition() + 1);
            change.setTargetLine(delta.getTarget().getPosition() + 1);
            change.setOriginalLines(new ArrayList<>(delta.getSource().getLines()));
            change.setRevisedLines(new ArrayList<>(delta.getTarget().getLines()));
            result.getChanges().add(change);
        }

        return result;
    }

    /**
     * 构建 Git 风格的左右对比 diff
     */
    private List<DiffLine> buildGitStyleDiff(List<String> originalLines, List<String> revisedLines, Patch<String> patch) {
        List<DiffLine> result = new ArrayList<>();
        int origIdx = 0;
        int revIdx = 0;

        for (AbstractDelta<String> delta : patch.getDeltas()) {
            int origDeltaStart = delta.getSource().getPosition();
            int revDeltaStart = delta.getTarget().getPosition();

            // 添加差异之前的相同内容
            while (origIdx < origDeltaStart && revIdx < revDeltaStart) {
                result.add(new DiffLine("EQUAL", originalLines.get(origIdx), revisedLines.get(revIdx)));
                origIdx++;
                revIdx++;
            }

            // 处理差异块
            DeltaType type = delta.getType();
            List<String> origLines = delta.getSource().getLines();
            List<String> revLines = delta.getTarget().getLines();

            if (type == DeltaType.INSERT) {
                // INSERT: 右侧新增，左侧为空
                for (String line : revLines) {
                    result.add(new DiffLine("INSERT", null, line));
                }
                revIdx += revLines.size();
            } else if (type == DeltaType.DELETE) {
                // DELETE: 左侧删除，右侧为空
                for (String line : origLines) {
                    result.add(new DiffLine("DELETE", line, null));
                }
                origIdx += origLines.size();
            } else if (type == DeltaType.CHANGE) {
                // CHANGE: 两侧都有内容
                int maxLines = Math.max(origLines.size(), revLines.size());
                for (int i = 0; i < maxLines; i++) {
                    String origLine = i < origLines.size() ? origLines.get(i) : null;
                    String revLine = i < revLines.size() ? revLines.get(i) : null;
                    result.add(new DiffLine("CHANGE", origLine, revLine));
                }
                origIdx += origLines.size();
                revIdx += revLines.size();
            }
        }

        // 添加最后一个差异块之后的相同内容
        while (origIdx < originalLines.size() && revIdx < revisedLines.size()) {
            result.add(new DiffLine("EQUAL", originalLines.get(origIdx), revisedLines.get(revIdx)));
            origIdx++;
            revIdx++;
        }

        // 处理剩余的行（一边还有内容）
        while (origIdx < originalLines.size()) {
            result.add(new DiffLine("DELETE", originalLines.get(origIdx), null));
            origIdx++;
        }
        while (revIdx < revisedLines.size()) {
            result.add(new DiffLine("INSERT", null, revisedLines.get(revIdx)));
            revIdx++;
        }

        return result;
    }
}

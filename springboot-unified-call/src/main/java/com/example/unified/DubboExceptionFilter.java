package com.example.unified;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.extension.Activate;
import com.example.unified.exception.BusinessException;
import org.apache.dubbo.rpc.*;

import java.util.function.BiConsumer;

@Activate(group = Constants.PROVIDER)
public class DubboExceptionFilter implements Filter {
    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        try {
            AsyncRpcResult result = (AsyncRpcResult )invoker.invoke(invocation);
            if (result.hasException()) {
                Throwable exception = result.getException();
                if (exception instanceof BusinessException) {
                    BusinessException e = (BusinessException) exception;
                    return new AppResponse (ApiResponse.fail(e.getCode(), e.getMessage()));
                }
            }

            return result.whenCompleteWithContext(new BiConsumer<Result, Throwable>() {
                @Override
                public void accept(Result result, Throwable throwable) {
                    result.setValue(ApiResponse.success(result.getValue()));
                }
            });

        } catch (Exception e) {
            return new AppResponse (ApiResponse.fail("500", "RPC调用异常"));
        }
    }
}
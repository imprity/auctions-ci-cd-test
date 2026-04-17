package com.example.auction.common.exception;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.WebRequest;
import org.springframework.boot.webmvc.error.DefaultErrorAttributes;

import java.util.HashMap;
import java.util.Map;

/**
 * {@link GlobalExceptionHandler}에서 잡지 못한 에러들을 잡아 저희가 정한 API 형식으로 바꾸어 주는 서비스 입니다.
 * <p>
 * 자세한 내용은 {@link DefaultErrorAttributes}를 확인해 주세요.
 */
@Component
public class CustomErrorAttributes extends DefaultErrorAttributes  {

    @Override
    public Map<String, @Nullable Object> getErrorAttributes(WebRequest webRequest, ErrorAttributeOptions options) {
        Map<String, Object> newErrorAttributes = new HashMap<>();

        newErrorAttributes.put("data", null);
        newErrorAttributes.put("success", false);
        addStatus(newErrorAttributes, webRequest);
        addMessage(newErrorAttributes, webRequest);

        return newErrorAttributes;
    }

    /**
     * status 필드를 추가합니다
     */
    private void addStatus(Map<String, Object> errorAttributes, RequestAttributes requestAttributes) {
        Integer status = this.getAttribute(requestAttributes, "jakarta.servlet.error.status_code");

        if (status == null) {
            errorAttributes.put("status", HttpStatus.INTERNAL_SERVER_ERROR.name());
        } else {
            try {
                errorAttributes.put("status", HttpStatus.valueOf(status).name());
            } catch (Exception err) {
                errorAttributes.put("status", HttpStatus.INTERNAL_SERVER_ERROR.name());
            }
        }
    }

    /**
     * message 필드를 추가합니다
     */
    private void addMessage(Map<String, Object> errorAttributes, RequestAttributes requestAttributes) {
        Integer status = this.getAttribute(requestAttributes, "jakarta.servlet.error.status_code");

        if (status == null) {
            errorAttributes.put("message", HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
        } else {
            try {
                errorAttributes.put("status", HttpStatus.valueOf(status).getReasonPhrase());
            } catch (Exception err) {
                errorAttributes.put("status", HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <T> @Nullable T getAttribute(RequestAttributes requestAttributes, String name) {
        return (T) requestAttributes.getAttribute(name, RequestAttributes.SCOPE_REQUEST);
    }
}

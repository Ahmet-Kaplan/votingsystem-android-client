package org.votingsystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.votingsystem.util.ResponseVS;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageDto {

    private Integer statusCode;
    private String message;
    private String URL;

    public MessageDto() {}

    public MessageDto(Integer statusCode, String message, String URL) {
        this.statusCode = statusCode;
        this.message = message;
        this.URL = URL;
    }

    public static MessageDto OK(String message, String URL) {
        return new MessageDto(ResponseVS.SC_OK, message, URL);
    }

    public static MessageDto REQUEST_REPEATED(String message, String URL) {
        return new MessageDto(ResponseVS.SC_ERROR_REQUEST_REPEATED, message, URL);
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public String getMessage() {
        return message;
    }


    public String getURL() {
        return URL;
    }

    public void setURL(String URL) {
        this.URL = URL;
    }
}

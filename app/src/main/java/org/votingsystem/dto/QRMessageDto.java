package org.votingsystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.votingsystem.util.TypeVS;

import java.io.Serializable;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class QRMessageDto<T> implements Serializable {

    @JsonIgnore private TypeVS typeVS;
    @JsonIgnore private T data;
    private Long deviceId;
    private String UUID;

    public QRMessageDto() {}

    public QRMessageDto(DeviceVSDto deviceVSDto, TypeVS typeVS) {
        this.typeVS = typeVS;
        this.deviceId = deviceVSDto.getId();
        this.UUID = java.util.UUID.randomUUID().toString().substring(0,3);
    }

    public String getUUID() {
        return UUID;
    }

    public void setUUID(String UUID) {
        this.UUID = UUID;
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public TypeVS getTypeVS() {
        return typeVS;
    }

    public void setTypeVS(TypeVS typeVS) {
        this.typeVS = typeVS;
    }
}
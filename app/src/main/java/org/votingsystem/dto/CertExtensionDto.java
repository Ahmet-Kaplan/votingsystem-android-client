package org.votingsystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CertExtensionDto {

    private String deviceId;
    private String deviceName;
    private String email;
    private String mobilePhone;
    private DeviceVSDto.Type deviceType;


    public CertExtensionDto() {}

    public CertExtensionDto(String deviceId, String deviceName, String email, String phone, DeviceVSDto.Type deviceType) {
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.email = email;
        this.mobilePhone = phone;
        this.deviceType = deviceType;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMobilePhone() {
        return mobilePhone;
    }

    public void setMobilePhone(String mobilePhone) {
        this.mobilePhone = mobilePhone;
    }

    public DeviceVSDto.Type getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(DeviceVSDto.Type deviceType) {
        this.deviceType = deviceType;
    }
}

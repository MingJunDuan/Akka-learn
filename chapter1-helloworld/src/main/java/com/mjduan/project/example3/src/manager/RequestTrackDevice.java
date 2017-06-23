package com.mjduan.project.example3.src.manager;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Hans  2017-06-22 01:23
 */
@Getter
@Setter
@ToString
public class RequestTrackDevice {
    final String groupId;
    final String deviceId;

    public RequestTrackDevice(String groupId, String deviceId) {
        this.groupId = groupId;
        this.deviceId = deviceId;
    }
}

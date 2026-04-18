package com.chatapp.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Group implements Serializable {
    private static final long serialVersionUID = 1L;

    private String groupId;
    private String groupName;
    private List<String> members;

    public Group(String groupId, String groupName) {
        this(groupId, groupName, new ArrayList<>());
    }

    public Group(String groupId, String groupName, List<String> members) {
        this.groupId = groupId;
        this.groupName = groupName;
        this.members = members == null ? new ArrayList<>() : new ArrayList<>(members);
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public List<String> getMembers() {
        return members;
    }

    public void setMembers(List<String> members) {
        this.members = members == null ? new ArrayList<>() : new ArrayList<>(members);
    }
}

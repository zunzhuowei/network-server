package com.hbsoo.server.message.entity;

import com.hbsoo.server.NowServer;

import java.util.Date;

/**
 * 转发的消息
 * Created by zun.wei on 2024/6/19.
 */
public final class ForwardMessage {

    /**
     * 消息id,唯一
     */
    private Long id;
    /**
     * 原始消息包内容。
     */
    private byte[] originMessage;
    /**
     * 消息过期时间,默认永久生效
     */
    private Long expireTime = -1L;
    /**
     * 转发时间,默认立即转发
     */
    private Long forwardTime = -1L;
    /**
     * 转发的目标服务器类型
     */
    private String toServerType;
    /**
     * 转发的目标服务器id,-1表示根据key值匹配
     */
    private Integer toServerId = -1;
    /**
     * 根据key 转发到目标服务器
     */
    private Object forwardKey;
    /**
     * 消息所属服务器id
     */
    private Integer belongServerId;
    /**
     * 是否使用可用服务器模式
     */
    private Boolean useAvailableServer = false;

    public ForwardMessage(Long id, HBSPackage.Builder builder, Date expireDate, Date forwardDate,
                          String toServerType, Object forwardKey) {
        this.id = id;
        this.originMessage = builder.buildPackage();
        this.expireTime = expireDate.getTime();
        this.forwardTime = forwardDate.getTime();
        this.toServerType = toServerType;
        this.forwardKey = forwardKey;
        this.belongServerId = NowServer.getServerInfo().getId();
    }

    public ForwardMessage(Long id, HBSPackage.Builder builder, long expireTime, long forwardTime,
                          String toServerType, Object forwardKey) {
        this.id = id;
        this.originMessage = builder.buildPackage();
        this.expireTime = expireTime;
        this.forwardTime = forwardTime;
        this.toServerType = toServerType;
        this.forwardKey = forwardKey;
        this.belongServerId = NowServer.getServerInfo().getId();
    }

    public ForwardMessage(Long id, HBSPackage.Builder builder, int delayTime,
                          String toServerType, Object forwardKey) {
        this.id = id;
        this.originMessage = builder.buildPackage();
        this.expireTime = -1L;
        this.forwardTime = new Date().getTime() + (delayTime * 1000L);
        this.toServerType = toServerType;
        this.forwardKey = forwardKey;
        this.belongServerId = NowServer.getServerInfo().getId();
    }

    public ForwardMessage(Long id, HBSPackage.Builder builder, Date expireDate, int delayTime,
                          String toServerType, Object forwardKey) {
        this.id = id;
        this.originMessage = builder.buildPackage();
        this.expireTime = expireDate.getTime();
        this.forwardTime = new Date().getTime() + (delayTime * 1000L);
        this.toServerType = toServerType;
        this.forwardKey = forwardKey;
        this.belongServerId = NowServer.getServerInfo().getId();
    }

    public ForwardMessage() {

    }

    public Integer getToServerId() {
        return toServerId;
    }

    public void setToServerId(Integer toServerId) {
        this.toServerId = toServerId;
    }

    public Integer getBelongServerId() {
        return belongServerId;
    }

    public void setBelongServerId(Integer belongServerId) {
        this.belongServerId = belongServerId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public byte[] getOriginMessage() {
        return originMessage;
    }

    public void setOriginMessage(byte[] originMessage) {
        this.originMessage = originMessage;
    }

    public Long getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(Date expireTime) {
        this.expireTime = expireTime.getTime();
    }

    public Long getForwardTime() {
        return forwardTime;
    }

    public void setForwardTime(Date forwardTime) {
        this.forwardTime = forwardTime.getTime();
    }

    public String getToServerType() {
        return toServerType;
    }

    public void setToServerType(String toServerType) {
        this.toServerType = toServerType;
    }

    public void setExpireTime(Long expireTime) {
        this.expireTime = expireTime;
    }

    public void setForwardTime(Long forwardTime) {
        this.forwardTime = forwardTime;
    }

    public Object getForwardKey() {
        return forwardKey;
    }

    public void setForwardKey(Object forwardKey) {
        this.forwardKey = forwardKey;
    }

    public Boolean getUseAvailableServer() {
        return useAvailableServer;
    }

    public void setUseAvailableServer(Boolean useAvailableServer) {
        this.useAvailableServer = useAvailableServer;
    }
}

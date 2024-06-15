package com.hbsoo.gateway.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.util.Date;

/**
 * Created by zun.wei on 2024/6/15.
 */
@TableName("t_genealogy")
public class Genealogy {

    @TableId(type = IdType.AUTO)
    Long id;
    @TableField("create_time")
    Date createTime;
    @TableField("update_time")
    Date updateTime;

    String name;
    String nickname;
    @TableField("sort_num")
    Integer sortNum;
    @TableField("sort_word")
    String sortWord;
    Integer sex;
    Integer alive;
    String phone;
    @TableField("dead_time")
    String deadTime;
    String tomb;
    String wechat;
    String avatar;
    String openid;
    String address;
    String marry;
    @TableField("marry_addr")
    String marryAddr;
    @TableField("spouse_name")
    String spouseName;
    @TableField("belong_id")
    Long belongId;
    @TableField("belong_level")
    Integer belongLevel;


    @Override
    public String toString() {
        return "Genealogy{" +
                "id=" + id +
                ", createTime=" + createTime +
                ", updateTime=" + updateTime +
                ", name='" + name + '\'' +
                ", nickname='" + nickname + '\'' +
                ", sortNum='" + sortNum + '\'' +
                ", sortWord='" + sortWord + '\'' +
                ", sex=" + sex +
                ", alive=" + alive +
                ", phone='" + phone + '\'' +
                ", deadTime='" + deadTime + '\'' +
                ", tomb='" + tomb + '\'' +
                ", wechat='" + wechat + '\'' +
                ", avatar='" + avatar + '\'' +
                ", openid='" + openid + '\'' +
                ", address='" + address + '\'' +
                ", marry='" + marry + '\'' +
                ", marryAddr='" + marryAddr + '\'' +
                ", spouseName='" + spouseName + '\'' +
                ", belongId=" + belongId +
                ", belongLevel=" + belongLevel +
                '}';
    }

}

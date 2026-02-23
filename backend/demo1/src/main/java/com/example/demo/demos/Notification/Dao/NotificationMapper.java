package com.example.demo.demos.Notification.Dao;

import com.example.demo.demos.Notification.Pojo.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface NotificationMapper {
    int insertNotification(Notification notification);

    Notification findByMsgId(@Param("msgId") String msgId, @Param("userId") Long userId);

    int markRead(@Param("id") Long id, @Param("userId") Long userId);

    int markAllRead(@Param("userId") Long userId);

    Integer countUnread(@Param("userId") Long userId);

    java.util.List<Notification> listByUser(@Param("userId") Long userId, @Param("readStatus") Integer readStatus);

    int delete(@Param("id") Long id, @Param("userId") Long userId);
}

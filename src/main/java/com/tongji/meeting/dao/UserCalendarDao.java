package com.tongji.meeting.dao;

import com.tongji.meeting.model.Calendar;
import com.tongji.meeting.model.UserCalendar;
import com.tongji.meeting.model.UserDomain;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserCalendarDao {
    void createUCRelation(UserCalendar userCalendar);
    void quitCalendar(@Param("userId") Integer userId,@Param("calendarId") Integer calendarId);
    void setNoDisturb(@Param("userId") Integer userId,@Param("calendarId") Integer calendarId,@Param("disturb") Boolean disturb);
    void setDetailExposed(@Param("userId") Integer userId,@Param("calendarId") Integer calendarId,@Param("detailExposed") Boolean detailExposed);
//    List<UserDomain> getAllMembers(Integer calendarId);
}

package com.tongji.meeting.controller;

import com.tongji.meeting.model.*;
import com.tongji.meeting.service.EventService;
import com.tongji.meeting.util.redis.RedisUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

@Api("事件模块")
@RequestMapping("/api/Event")
@RestController
public class EventController {
    @Autowired
    private EventService eventService;

    @Autowired
    private RedisUtils redisUtils;
    //检查有权创建事件
    //在calendar里


    //再次检查创建权限
    @ApiOperation(value = "创建个人新事件", notes="某用户在某日历中创建新事件，加入组员不在这个api")
    @RequestMapping(value = "/addNewEvent" , method = RequestMethod.GET ,produces = "application/json")
    public ResponseEntity addNewEvent(
            @RequestParam(value = "title")
                    String title,
            @RequestParam(value = "content")
                    String content,
            @RequestParam(value = "priority")
                    int priority,
            @RequestParam(value = "start_time")
                    Date startTime,
            @RequestParam(value = "end_time")
                    Date endTime,
            @RequestParam(value = "calendar_id")
                    int calendarId,
            @RequestParam(value = "is_remind")
                    int isRemind,
            @RequestParam(value = "remind_time", required = false)
                    Date remindTime,
            @RequestParam(value = "is_repeat")
                    int isRepeat,
            @RequestParam(value = "repeat_unit",required = false)//"0"代表不重复，接受“day","week","month",,,,
                    String repeatUnit,
            @RequestParam(value = "repeat_amount",required = false)//每几天，每几周的”几“
                    Integer repeatAmount,
            @RequestHeader(value = "Authorization")
                    String sKey
    ){
        int userId = (int)redisUtils.hget(sKey, "userid");

        EventDetail eventDetail = new EventDetail();
        eventDetail.setStartTime(startTime);
        eventDetail.setTitle(title);
        eventDetail.setEndTime(endTime);
        eventDetail.setContent(content);
        eventDetail.setRepeat(isRepeat);

        Event event = new Event();
        event.setCalendarId(calendarId);
        event.setPriority(priority);
        event.setIsRemind(isRemind);

        UserDetail userDetail = new UserDetail();
        userDetail.setUserId(userId);

        EventReminder eventReminder = new EventReminder();
        EventRepetition eventRepetition =new EventRepetition();

        if (isRemind == 1) {
            eventReminder.setRemindTime(remindTime);
        }
        if (isRepeat == 1) {
            eventRepetition.setRepeatAmount(1);
            eventRepetition.setRepeatUnit("week");
        }

        eventService.addNewEvent(eventDetail, event, userDetail, eventReminder, eventRepetition);
        return ResponseEntity.ok("success");
    }



    //在修改页面前先调用一次权限查询
    @ApiOperation(value = "查看修改权限", notes="查看用户是否有可修改某事件权限，返回权限级别")
    @RequestMapping(value = "/checkPermission" , method = RequestMethod.GET ,produces = "application/json")
    public ResponseEntity checkPermission(
            @RequestParam(value = "detailId") int detailId,
            @RequestParam(value = "userId") int userId
    ){
        UserDetail userDetail = new UserDetail();
        userDetail.setUserId(userId);
        userDetail.setDetailId(detailId);
        //"owner" or "read"
        return ResponseEntity.ok(eventService.checkPermission(userDetail));
    }

    //需要再次核实权限
    @ApiOperation(value = "修改事件", notes="修改事件时间、标题、内容、优先级、移动日历，可能只有部分")
    @RequestMapping(value = "/modifyEvent" , method = RequestMethod.GET ,produces = "application/json")
    public ResponseEntity modifyEvent(
//            @RequestParam(value = "detailId") int detailId,
            @RequestParam(value = "event_id") int eventId,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "content", required = false) String content,
            @RequestParam(value = "priority", required = false) Integer priority,
            @RequestParam(value = "start_time", required = false) Date startTime,
            @RequestParam(value = "end_time", required = false) Date endTime,
//            @RequestParam(value = "calendarId", required = false) int calendarId,
            @RequestHeader(value = "Authorization")
                    String sKey
    ){
        int userId = (int)redisUtils.hget(sKey, "userid");
        Event newEvent = new Event();
        newEvent.setEventId(eventId);
        newEvent.setPriority(priority);

        Event oldEvent = new Event();
        oldEvent.setEventId(eventId);

        EventDetail eventDetail = new EventDetail();
        eventDetail.setContent(content);
        eventDetail.setTitle(title);
        eventDetail.setStartTime(startTime);
        eventDetail.setEndTime(endTime);

        UserDetail userDetail = new UserDetail();
        userDetail.setUserId(userId);

        eventService.modifyEvent(eventDetail, newEvent, oldEvent, userDetail);
        return ResponseEntity.ok("success");
    }

    @ApiOperation(value = "删除事件", notes="含：owner删除、reader删除")
    @RequestMapping(value = "/deleteEvent" , method = RequestMethod.GET ,produces = "application/json")
    public ResponseEntity deleteEvent(
            @RequestHeader(value = "Authorization",required = true)String sKey,
            @RequestParam(value = "event_id") int eventId
    ){
        int userId = (int)redisUtils.hget(sKey, "userid");
        UserDomain userDomain = new UserDomain();
        userDomain.setUserid(userId);

        Event event = new Event();
        event.setEventId(eventId);

        eventService.deleteEvent(event,userDomain);
        return ResponseEntity.ok("Success!");
    }


    @ApiOperation(value = "显示我个人日历事件", notes="我唯一的一个个人日历")
    @RequestMapping(value = "/showMyCalendar" , method = RequestMethod.GET ,produces = "application/json")
    public ResponseEntity show1CalendarEvents(
            @RequestHeader(value = "Authorization")String sKey
    ){
        int userId = (int)redisUtils.hget(sKey, "userid");
        return ResponseEntity.ok(eventService.showMyCalendar(userId));
    }

    @ApiOperation(value = "发送共享事件邀请", notes="")
    @RequestMapping(value = "/shareEvent" , method = RequestMethod.GET ,produces = "application/json")
    public ResponseEntity shareEvent(
            @RequestParam(value = "userId") int userId
    ){
        return ResponseEntity.ok("Success!");
    }

    @ApiOperation(value = "接受共享事件邀请", notes="")
    @RequestMapping(value = "/acceptEvent" , method = RequestMethod.GET ,produces = "application/json")
    public ResponseEntity acceptEvent(
            @RequestParam(value = "userId") int userId
    ){
        return ResponseEntity.ok("Success!");
    }

    @ApiOperation(value = "推荐时间", notes="")
    @RequestMapping(value = "/recommend" , method = RequestMethod.GET ,produces = "application/json")
    public ResponseEntity recommend(
            @RequestParam(value = "calendarId") int calendarId,
            @RequestParam(value = "duration") int duration
//            @RequestParam(value = "priority") int priority
            ){
        return ResponseEntity.ok(eventService.recommend(calendarId,duration));
    }

    @ApiOperation(value = "创建工作组新事件", notes="检查是否存在优先级冲突")
    @RequestMapping(value = "/addGroupEvent" , method = RequestMethod.GET ,produces = "application/json")
    public ResponseEntity addGroupvent(
            @RequestParam(value = "title")
                    String title,
            @RequestParam(value = "content")
                    String content,
            @RequestParam(value = "priority")
                    int priority,
            @RequestParam(value = "start_time")
                    Date startTime,
            @RequestParam(value = "end_time")
                    Date endTime,
            @RequestParam(value = "calendar_id")
                    int calendarId,
            @RequestParam(value = "is_remind")
                    int isRemind,
            @RequestParam(value = "remind_time", required = false)
                    Date remindTime,
            @RequestParam(value = "is_repeat")
                    int isRepeat,
            @RequestParam(value = "repeat_unit",required = false)//"0"代表不重复，接受“day","week","month",,,,
                    String repeatUnit,
            @RequestParam(value = "repeat_amount",required = false)//每几天，每几周的”几“
                    Integer repeatAmount,
            @RequestHeader(value = "Authorization")
                    String sKey
    ){
        int userId = (int)redisUtils.hget(sKey, "userid");

        EventDetail eventDetail = new EventDetail();
        eventDetail.setStartTime(startTime);
        eventDetail.setTitle(title);
        eventDetail.setEndTime(endTime);
        eventDetail.setContent(content);
        eventDetail.setRepeat(isRepeat);

        Event event = new Event();
        event.setCalendarId(calendarId);
        event.setPriority(priority);
        event.setIsRemind(isRemind);

        UserDetail userDetail = new UserDetail();
        userDetail.setUserId(userId);

        EventReminder eventReminder = new EventReminder();
        EventRepetition eventRepetition =new EventRepetition();

        if (isRemind == 1) {
            eventReminder.setRemindTime(remindTime);
        }
        if (isRepeat == 1) {
            eventRepetition.setRepeatAmount(1);
            eventRepetition.setRepeatUnit("week");
        }

        return ResponseEntity.ok(eventService.addGroupEvent(
                eventDetail, event, userDetail, eventReminder, eventRepetition));
    }

    @ApiOperation(value = "显示组里某人日历事件", notes="根据隐私来")
    @RequestMapping(value = "/showOthersEvent" , method = RequestMethod.GET ,produces = "application/json")
    public ResponseEntity showOthersEvent(
            @RequestParam(value = "userid") int userId,
            @RequestHeader(value = "Authorization")
                    String sKey
    ){
        System.out.println("=!!!!!!进入controller=");
        return ResponseEntity.ok(eventService.showOthersEvent(userId));
    }
}

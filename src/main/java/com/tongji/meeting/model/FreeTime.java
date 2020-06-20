package com.tongji.meeting.model;

import com.tongji.meeting.util.TimePeriod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FreeTime {
    private List<EventAllInfo> eventDetailList;

    public FreeTime(List<EventAllInfo> eventDetailList) {
        this.eventDetailList = eventDetailList;
    }

    public List<TimePeriod> computeFreeTime(){
        List<TimePeriod> occupied = new ArrayList<>();;
        for (EventAllInfo item : eventDetailList) {
            occupied.add(new TimePeriod(item.getStartTime(),item.getEndTime()));
        }

        Collections.sort(occupied);
        //
        return new ArrayList<>();
    }
}
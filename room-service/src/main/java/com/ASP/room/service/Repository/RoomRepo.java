package com.ASP.room.service.Repository;

import com.ASP.room.service.Entity.Room;
import com.ASP.room.service.Entity.RoomStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface RoomRepo extends JpaRepository<Room, Long> {

    List<Room> findByStatusIn(List<RoomStatus> statuses);
}

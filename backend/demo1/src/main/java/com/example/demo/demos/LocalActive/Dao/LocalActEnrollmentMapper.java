package com.example.demo.demos.LocalActive.Dao;

import com.example.demo.demos.LocalActive.DTO.LocalActEnrollmentActionRecord;
import com.example.demo.demos.LocalActive.DTO.LocalActEnrollmentQuery;
import com.example.demo.demos.LocalActive.DTO.LocalActEnrollmentRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface LocalActEnrollmentMapper {
    List<LocalActEnrollmentRecord> findEnrollments(LocalActEnrollmentQuery query);

    LocalActEnrollmentActionRecord findLatestEnrollment(@Param("activityId") Long activityId,
                                                        @Param("userId") Integer userId);

    int countConfirmed(@Param("activityId") Long activityId);

    int countWaitlist(@Param("activityId") Long activityId);

    int insertEnrollment(@Param("activityId") Long activityId,
                         @Param("userId") Integer userId,
                         @Param("status") String status,
                         @Param("waitlistRank") Integer waitlistRank);

    int updateEnrollmentStatus(@Param("id") Long id,
                               @Param("status") String status,
                               @Param("waitlistRank") Integer waitlistRank);

    LocalActEnrollmentActionRecord findById(@Param("id") Long id);

    LocalActEnrollmentActionRecord findFirstWaitlist(@Param("activityId") Long activityId);

    int decrementWaitlistRanksAfter(@Param("activityId") Long activityId,
                                    @Param("waitlistRank") Integer waitlistRank);
}

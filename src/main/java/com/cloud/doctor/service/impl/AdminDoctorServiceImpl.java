package com.cloud.doctor.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cloud.doctor.entity.Doctor;
import com.cloud.doctor.entity.dto.DoctorReq;
import com.cloud.doctor.entity.vo.DoctorVO;
import com.cloud.doctor.mapper.DoctorMapper;
import com.cloud.doctor.service.AdminDoctorService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminDoctorServiceImpl implements AdminDoctorService {

    private final DoctorMapper doctorMapper;

    private final RedisTemplate redisTemplate;

    @Override
    public void addDoctor(DoctorReq req) {
        Doctor doctor = Doctor.builder()
                .realName(req.realName())
                .deptId(req.deptId())
                .title(req.title())
                .intro(req.intro())
                .consultPrice(req.consultPrice())
                .status(req.status())
                .build();
        //新增医生
        doctorMapper.insert(doctor);
        //删除缓存
        String cacheKey = "hospital:doctor:" + doctor.getDeptId();
        redisTemplate.delete(cacheKey);
    }

    @Override
    public void deleteDoctor(Long id) {
        //删除医生
        doctorMapper.deleteById(id);
        //删除缓存
        String cacheKey = "hospital:doctor:" + id;
        redisTemplate.delete(cacheKey);
    }

    @Override
    public List<DoctorVO> selectDoctor() {
        List<Doctor> doctors = doctorMapper.selectList(null);
        return doctors.stream().map(doctor -> {
            DoctorVO doctorVO = new DoctorVO();
            BeanUtils.copyProperties(doctor, doctorVO);
            return doctorVO;
        }).collect(Collectors.toList());
    }
}

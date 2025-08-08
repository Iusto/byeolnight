package com.byeolnight.dto.shop;

import com.byeolnight.entity.shop.StellaIconGrade;
import lombok.Data;
/*
 시스템 초기화 (JSON 데이터 로드)
 */
@Data
public class StellaIconDataDto {
    private String name;
    private String description;
    private String iconUrl;
    private StellaIconGrade grade;
    private int price;
    private boolean available;
}
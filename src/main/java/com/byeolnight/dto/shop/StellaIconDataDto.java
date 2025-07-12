package com.byeolnight.dto.shop;

import com.byeolnight.domain.entity.shop.StellaIconGrade;
import lombok.Data;

@Data
public class StellaIconDataDto {
    private String name;
    private String description;
    private String iconUrl;
    private StellaIconGrade grade;
    private int price;
    private boolean available;
}
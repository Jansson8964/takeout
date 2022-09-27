package com.mashang.reggie.dto;

import com.mashang.reggie.entity.Dish;
import com.mashang.reggie.entity.DishFlavor;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;

/**
 * dto
 * 数据传输对象 data transfer object
 */
@Data
public class DishDto extends Dish {

    private List<DishFlavor> flavors = new ArrayList<>();

    private String categoryName;

    private Integer copies;
}

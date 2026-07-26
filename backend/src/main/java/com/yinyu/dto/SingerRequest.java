package com.yinyu.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 后台歌手新增/修改（api.md 18.4.2/18.4.3）
 */
@Data
public class SingerRequest {

    @Size(max = 100, message = "歌手名过长")
    private String name;

    private String avatar;
    /** 地区：内地/港台/欧美/日韩/其他 */
    private String area;
    /** 1-男 2-女 3-组合 */
    private Integer type;
    private String intro;
}

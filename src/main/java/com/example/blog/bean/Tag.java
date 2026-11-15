package com.example.blog.bean;

import java.io.Serializable;

/** 文章标签，与文章是多对多关系（中间表 post_tags）。 */
public class Tag implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private String name;
    /** 该标签被多少篇文章使用（统计用，右侧标签云按它排序）。 */
    private int postCount;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPostCount() {
        return postCount;
    }

    public void setPostCount(int postCount) {
        this.postCount = postCount;
    }
}
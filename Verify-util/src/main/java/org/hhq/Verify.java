package org.hhq;

import org.hhq.verify.NotEmpty;
import org.hhq.verify.Range;

public class Verify {

    private String describe;

    @NotEmpty(describe = "名称不能为空")
    private String name;
    @Range(max = 20, min = 1, describe = "kkk")
    private int age;

    public String getDescribe() {
        return describe;
    }

    public void setDescribe(String describe) {
        this.describe = describe;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }
}

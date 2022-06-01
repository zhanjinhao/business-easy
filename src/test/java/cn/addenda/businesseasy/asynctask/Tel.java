package cn.addenda.businesseasy.asynctask;

import lombok.Setter;

/**
 * @author 01395265
 * @date 2022/5/25
 */
@Setter
public class Tel {

    private String group1;
    private String group2;
    private String group3;
    private String group4;

    public String assembleTel() {
        return "group1 : " + group1 + "\n"
            + "group2 : " + group2 + "\n"
            + "group3 : " + group3 + "\n"
            + "group4 : " + group4;
    }

}

package cn.addenda.businesseasy.pojo;

import lombok.*;

/**
 * 二元
 *
 * @author addenda
 * @datetime 2023/1/21 16:00
 */
@Setter
@Getter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class Binary<T1, T2> {

    T1 f1;

    T2 f2;

}

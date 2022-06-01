package cn.addenda.businesseasy.asynctask;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * @author 01395265
 * @date 2022/5/25
 */
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class BinaryResult<R1, R2> implements Result<BinaryResult<R1, R2>> {

    private R1 firstResult;
    private R2 secondResult;

}

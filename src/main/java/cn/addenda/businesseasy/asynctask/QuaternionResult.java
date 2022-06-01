package cn.addenda.businesseasy.asynctask;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * @author 01395265
 * @date 2022/5/27
 */
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class QuaternionResult<R1, R2, R3, R4> implements Result<QuaternionResult<R1, R2, R3, R4>> {

    private R1 firstResult;
    private R2 secondResult;
    private R3 thirdResult;
    private R4 fourthResult;

}

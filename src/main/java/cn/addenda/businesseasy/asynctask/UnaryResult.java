package cn.addenda.businesseasy.asynctask;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * @author 01395265
 * @date 2022/5/31
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class UnaryResult<R1> implements Result<UnaryResult<R1>> {

    private R1 firstResult;

}

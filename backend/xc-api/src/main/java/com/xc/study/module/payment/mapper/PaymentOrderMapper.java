package com.xc.study.module.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xc.study.module.payment.entity.PaymentOrder;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PaymentOrderMapper extends BaseMapper<PaymentOrder> {

    @Select("""
            select coalesce(sum(amount), 0)
            from payment_orders
            where status = 'paid'
              and paid_at >= #{from}
            """)
    BigDecimal sumPaidAmountFrom(@Param("from") OffsetDateTime from);
}

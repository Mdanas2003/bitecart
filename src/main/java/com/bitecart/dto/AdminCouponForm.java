package com.bitecart.dto;

import com.bitecart.entity.DiscountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AdminCouponForm {
    @NotBlank @Size(max = 50) private String code;
    @Size(max = 1000) private String description;
    @NotNull private DiscountType discountType;
    @NotNull @DecimalMin(value = "0.00", inclusive = false) private BigDecimal discountValue;
    @NotNull @DecimalMin("0.00") private BigDecimal minimumOrderAmount = BigDecimal.ZERO;
    @DecimalMin(value = "0.00", inclusive = false) private BigDecimal maximumDiscount;
    @NotNull @Future private LocalDateTime expiryDate;
    @Min(1) private Integer usageLimit;
    private boolean active = true;
    public String getCode() { return code; } public void setCode(String value) { code = value; }
    public String getDescription() { return description; } public void setDescription(String value) { description = value; }
    public DiscountType getDiscountType() { return discountType; } public void setDiscountType(DiscountType value) { discountType = value; }
    public BigDecimal getDiscountValue() { return discountValue; } public void setDiscountValue(BigDecimal value) { discountValue = value; }
    public BigDecimal getMinimumOrderAmount() { return minimumOrderAmount; } public void setMinimumOrderAmount(BigDecimal value) { minimumOrderAmount = value; }
    public BigDecimal getMaximumDiscount() { return maximumDiscount; } public void setMaximumDiscount(BigDecimal value) { maximumDiscount = value; }
    public LocalDateTime getExpiryDate() { return expiryDate; } public void setExpiryDate(LocalDateTime value) { expiryDate = value; }
    public Integer getUsageLimit() { return usageLimit; } public void setUsageLimit(Integer value) { usageLimit = value; }
    public boolean isActive() { return active; } public void setActive(boolean value) { active = value; }
}

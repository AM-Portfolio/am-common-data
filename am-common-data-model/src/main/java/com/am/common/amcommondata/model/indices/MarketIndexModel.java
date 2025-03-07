package com.am.common.amcommondata.model.indices;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketIndexModel {
    private UUID id;
    private String code;
    private String name;
    private String description;
    private IndexType indexType;
    private boolean active;
    private List<EquityIndexMembershipModel> indexMemberships;
}

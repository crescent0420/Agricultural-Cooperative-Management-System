package com.acms.service;

import com.acms.common.BaseService;
import com.acms.common.PagedResponse;
import com.acms.common.Paging;
import com.acms.dto.ProvinceResponse;
import com.acms.repository.ProvinceRepository;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProvinceService extends BaseService {

    private static final Set<String> SORTABLE_FIELDS = Set.of("id", "name");

    private final ProvinceRepository provinceRepository;

    @Transactional(readOnly = true)
    public PagedResponse<ProvinceResponse> findAll(int page, int size, String sortBy, String direction) {
        return PagedResponse.from(provinceRepository
                .findAll(Paging.of(page, size, sortBy, direction, SORTABLE_FIELDS))
                .map(ProvinceResponse::from));
    }
}

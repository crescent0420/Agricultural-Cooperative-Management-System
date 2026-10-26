
package com.acms.service;

import com.acms.dto.PageResponse;
import com.acms.dto.TrainingRequest;
import com.acms.dto.TrainingResponse;
import com.acms.entity.Training;
import com.acms.exception.ConflictException;
import com.acms.exception.ResourceNotFoundException;
import com.acms.repository.TrainingRepository;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TrainingService {

    private static final Set<String> SORTABLE_FIELDS =
            Set.of("id", "name", "createdAt");

    private final TrainingRepository trainingRepository;

    public TrainingService(TrainingRepository trainingRepository) {
        this.trainingRepository = trainingRepository;
    }

    @Transactional
    public TrainingResponse create(TrainingRequest request) {
        String name = request.name().trim();

        if (trainingRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException(
                    "Training '" + name + "' already exists.");
        }

        Training training = new Training(name);
        return TrainingResponse.from(trainingRepository.save(training));
    }

    @Transactional(readOnly = true)
    public PageResponse<TrainingResponse> findAll(
            int page, int size, String sortBy, String direction) {

        Pageable pageable = Paging.of(
                page, size, sortBy, direction, SORTABLE_FIELDS);

        return PageResponse.from(
                trainingRepository.findAll(pageable)
                        .map(TrainingResponse::from));
    }

    @Transactional(readOnly = true)
    public TrainingResponse findById(Long id) {
        return TrainingResponse.from(getOrThrow(id));
    }

    @Transactional
    public TrainingResponse rename(Long id, TrainingRequest request) {
        Training training = getOrThrow(id);
        String name = request.name().trim();

        if (trainingRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ConflictException(
                    "Training '" + name + "' already exists.");
        }

        training.setName(name);
        return TrainingResponse.from(training);
    }

    private Training getOrThrow(Long id) {
        return trainingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Training with id " + id + " was not found."));
    }
}

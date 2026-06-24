/*  // This Java source file contains the VisitResource class, which is a Spring REST controller for managing visit operations. The file includes the Apache License header, necessary Spring MVC imports (e.g., PutMapping, DeleteMapping, RestController), and defines the VisitResource class. The file is part of the REST API layer and utilizes the @Timed annotation for metrics configuration.
 * Copyright 2002-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.samples.petclinic.visits.web;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

import io.micrometer.core.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.samples.petclinic.visits.model.Visit;
import org.springframework.samples.petclinic.visits.model.VisitRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * The VisitResource class is a REST controller responsible for handling visit-related CRUD operations. It is annotated with @RestController and @Timed('petclinic.visit') for metrics configuration. The class includes a logger for logging and a VisitRepository dependency injected via constructor. It defines endpoints for creating (POST), reading (GET), updating (PUT), and deleting (DELETE) visits, all interacting with the Visit entity class. Error handling uses ResourceNotFoundException for missing visits. A nested Visits record is used to return lists of visits.
 */
@RestController
@Timed("petclinic.visit")
class VisitResource {

    private static final Logger log = LoggerFactory.getLogger(VisitResource.class);

    private final VisitRepository visitRepository;

    VisitResource(VisitRepository visitRepository) {
        this.visitRepository = visitRepository;
    }

    @PostMapping("owners/*/pets/{petId}/visits")
    @ResponseStatus(HttpStatus.CREATED)
    public Visit create(
        @Valid @RequestBody Visit visit,
        @PathVariable("petId") @Min(1) int petId) {

        visit.setPetId(petId);
        log.info("Saving visit {}", visit);
        return visitRepository.save(visit);
    }

    @GetMapping("owners/*/pets/{petId}/visits")
    public List<Visit> read(@PathVariable("petId") @Min(1) int petId) {
        return visitRepository.findByPetId(petId);
    }

    /**
     * This GET endpoint retrieves visits by a list of pet IDs. It uses the @GetMapping annotation with path pattern 'pets/visits' and accepts a request parameter petId of type List<Integer>. The method calls visitRepository.findByPetIdIn to fetch the visits and returns them wrapped in a Visits record, enabling the REST API layer to provide visit data based on pet IDs.
     */
    @GetMapping("pets/visits")
    public Visits read(@RequestParam("petId") List<Integer> petIds) {
        final List<Visit> byPetIdIn = visitRepository.findByPetIdIn(petIds);
        return new Visits(byPetIdIn);
    }

    @GetMapping("vets/{vetId}/visits")
    public Visits readByVet(@PathVariable("vetId") @Min(1) int vetId) {
        return new Visits(visitRepository.findByVetId(vetId));
    }

    /**
     * This PUT endpoint updates a visit's description and/or date. It uses the @PutMapping annotation with path pattern 'owners/*/pets/{petId}/visits/{visitId}' and returns HTTP 204 No Content. The method accepts a validated Visit entity object in the request body and path variables petId and visitId, both with @Min(1) validation. It retrieves the existing Visit by visitId using visitRepository.findById, throwing a ResourceNotFoundException if not found, then updates the description and conditionally sets the date, logs the update, and saves the modified visit to the repository.
     */
    @PutMapping("owners/*/pets/{petId}/visits/{visitId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void update(
        @Valid @RequestBody Visit visit,
        @PathVariable("petId") @Min(1) int petId,
        @PathVariable("visitId") @Min(1) int visitId) {

        Visit existing = visitRepository.findById(visitId)
            .orElseThrow(() -> new ResourceNotFoundException("Visit " + visitId + " not found"));
        existing.setDescription(visit.getDescription());
        if (visit.getDate() != null) {
            existing.setDate(visit.getDate());
        }
        log.info("Updating visit {}", existing);
        visitRepository.save(existing);
    }

    /**
     * This DELETE endpoint deletes a visit by ID. It uses the @DeleteMapping annotation with path pattern 'owners/*/pets/{petId}/visits/{visitId}' and returns HTTP 204 No Content. The method takes path variables petId and visitId with @Min(1) validation, retrieves the Visit entity by visitId using visitRepository.findById, throwing a ResourceNotFoundException if not found, logs the deletion, and deletes the visit from the repository.
     */
    @DeleteMapping("owners/*/pets/{petId}/visits/{visitId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
        @PathVariable("petId") @Min(1) int petId,
        @PathVariable("visitId") @Min(1) int visitId) {

        Visit existing = visitRepository.findById(visitId)
            .orElseThrow(() -> new ResourceNotFoundException("Visit " + visitId + " not found"));
        log.info("Deleting visit {}", existing);
        visitRepository.delete(existing);
    }

    record Visits(
        List<Visit> items
    ) {
    }
}

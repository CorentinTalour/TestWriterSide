package fr.formation.Projet_Grp_Java.api;

import fr.formation.Projet_Grp_Java.exception.CarNotFoundException;
import fr.formation.Projet_Grp_Java.exception.CarStatusNotFoundException;
import fr.formation.Projet_Grp_Java.exception.CompanyNotFoundException;
import fr.formation.Projet_Grp_Java.model.Car;
import fr.formation.Projet_Grp_Java.model.CarStatus;
import fr.formation.Projet_Grp_Java.model.Company;
import fr.formation.Projet_Grp_Java.repo.CarRepository;
import fr.formation.Projet_Grp_Java.repo.CarStatusRepository;
import fr.formation.Projet_Grp_Java.repo.CompanyRepository;
import fr.formation.Projet_Grp_Java.request.CreateOrUpdateCarRequest;
import fr.formation.Projet_Grp_Java.response.CarByIdResponse;
import fr.formation.Projet_Grp_Java.response.CarResponse;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.BeanUtils;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;


import java.util.List;

@RestController
@RequestMapping("/api/car")
@RequiredArgsConstructor
@Log4j2
public class CarApiController {
    private final CarRepository repository;
    private final CarStatusRepository carStatusRepository;
    private final CompanyRepository companyRepository;

    @PostConstruct
    public void init() {
        // Vérifiez si la table est vide
        if (carStatusRepository.count() == 0) {
            CarStatus carStatusDispo = new CarStatus();
            carStatusDispo.setId("1");
            carStatusDispo.setCarStatusName("Disponible");
            carStatusRepository.save(carStatusDispo);

            CarStatus carStatusLouee = new CarStatus();
            carStatusLouee.setId("2");
            carStatusLouee.setCarStatusName("Louée");
            carStatusRepository.save(carStatusLouee);


            CarStatus carStatusMaintenance = new CarStatus();
            carStatusMaintenance.setId("3");
            carStatusMaintenance.setCarStatusName("Maintenance");
            carStatusRepository.save(carStatusMaintenance);
        }
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<CarResponse> findAll() {
        log.debug("Finding all car ...");

        return this.repository.findAll()
                .stream()
                .map(this::convert)
                .toList();
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
        public CarByIdResponse findById(@PathVariable String id) {
        log.debug("Finding car {} ...", id);

        Car car = this.repository.findById(id).orElseThrow(CarNotFoundException::new);
        CarByIdResponse resp = CarByIdResponse.builder().build();

        BeanUtils.copyProperties(car, resp);

        return resp;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public String create(@Valid @RequestBody CreateOrUpdateCarRequest request) {
        log.debug("Creating car ...");

        Car car = new Car();
        BeanUtils.copyProperties(request, car);

        CarStatus carStatus = carStatusRepository.findById(request.getCarStatusId())
                .orElseThrow(CarStatusNotFoundException::new);
        car.setCarStatus(carStatus);

        Company companyId = companyRepository.findById(request.getCompanyId())
                .orElseThrow(CompanyNotFoundException::new);
        car.setCompany(companyId);

        this.repository.save(car);

        log.debug("Car {} created!", car.getId());

        return car.getId();
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('ADMIN')")
    public String update(@PathVariable String id, @Valid @RequestBody CreateOrUpdateCarRequest request) {
        log.debug("Updating car {} ...", id);

        Car car = this.repository.findById(id).orElseThrow(CarNotFoundException::new);

        BeanUtils.copyProperties(request, car);

        if (request.getCarStatusId() != null) {
            log.debug("Looking for CarStatus with ID: {}", request.getCarStatusId());

            CarStatus carStatus = carStatusRepository.findById(request.getCarStatusId())
                    .orElseThrow(CarStatusNotFoundException::new);
            car.setCarStatus(carStatus);
        }

        if (request.getCompanyId() != null) {
            log.debug("Looking for Company with ID: {}", request.getCompanyId());

            Company company = companyRepository.findById(request.getCompanyId())
                    .orElseThrow(CompanyNotFoundException::new);
            car.setCompany(company);
        }
        this.repository.save(car);

        log.debug("Car {} updated!", id);

        return car.getId();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteById(@PathVariable String id) {
        log.debug("Deleting car {} ...", id);

        try {
            this.repository.deleteById(id);
        }

        catch (Exception ex) {
            log.error("Can't delete car {}! Comments may exist!", id);
        }

        log.debug("car {} deleted!", id);
    }

    private CarResponse convert(Car car) {
        CarResponse resp = CarResponse.builder().build();

        BeanUtils.copyProperties(car, resp);

        return resp;
    }
}
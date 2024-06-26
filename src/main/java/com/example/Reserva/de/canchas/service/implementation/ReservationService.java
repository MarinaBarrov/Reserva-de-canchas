package com.example.Reserva.de.canchas.service.implementation;

import com.example.Reserva.de.canchas.entity.domain.Sport;
import com.example.Reserva.de.canchas.entity.dto.ReservationRequestDTO;
import com.example.Reserva.de.canchas.entity.domain.Reservation;
import com.example.Reserva.de.canchas.entity.domain.SportField;
import com.example.Reserva.de.canchas.entity.dto.ReservationRequestUpdateDTO;
import com.example.Reserva.de.canchas.entity.dto.ReservationResponseDTO;
import com.example.Reserva.de.canchas.exception.ResourceNotFoundException;
import com.example.Reserva.de.canchas.repository.IReservationRepository;
import com.example.Reserva.de.canchas.repository.ISportFieldRepository;
import com.example.Reserva.de.canchas.service.IReservationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ReservationService implements IReservationService {

    @Autowired
    IReservationRepository reservationRepository;
    @Autowired
    ISportFieldRepository sportFieldRepository;

    @Autowired
    ObjectMapper mapper;

    @Override
    public ReservationResponseDTO guardar(ReservationRequestDTO reservationRequestDTO) {
        Reservation reservation = mapper.convertValue(reservationRequestDTO, Reservation.class);
        Set<SportField> sportFieldList = sportFieldRepository.findBySport(reservationRequestDTO.getSport());

        SportField sportFieldFound = null;

        for (SportField sportField : sportFieldList) {
            Boolean isAvailable = sportField.getAvailability().get(reservation.getHour());
            if (isAvailable != null && isAvailable) {
                //TODO: buscar si existe reservas para esa fecha en esa cancha

                sportFieldFound = sportField;
                break;
            }
        }
        if (sportFieldFound == null) {
            throw new ResourceNotFoundException("There is not sport field available at the requested time");
        }
        reservation.setSportField(sportFieldFound);
        Reservation reservationSaved = reservationRepository.save(reservation);
        return mapper.convertValue(reservationSaved, ReservationResponseDTO.class);
    }

    @Override
    public Set<ReservationRequestDTO> listarTodos() {
        List<Reservation> reservations = reservationRepository.findAll();
        if (reservations.isEmpty()) {
            throw new RuntimeException("No se encontraron reservas para listar.");
        }
        Set<ReservationRequestDTO> reservationRequestDTOS = new HashSet<>();

        for (Reservation reservation : reservations) {
            reservationRequestDTOS.add(mapper.convertValue(reservation, ReservationRequestDTO.class));
        }
        return reservationRequestDTOS;
    }


    @Override
    public ReservationRequestDTO buscarPorId(Integer id) {
        Optional<Reservation> reservation = reservationRepository.findById(id);
        ReservationRequestDTO reservationRequestDTO = null;
        if (reservation.isPresent()) {
            reservationRequestDTO = mapper.convertValue(reservation, ReservationRequestDTO.class);
        } else {

            throw new RuntimeException("No se encontro la reserva con id " + id);

        }
        return reservationRequestDTO;
    }

    @Override
    public List<ReservationResponseDTO> search(Sport sport, String sportFieldName) {

        List<Reservation> reservations = reservationRepository.findByCriteria(sport, sportFieldName);

        if (reservations.isEmpty()) {
            throw new ResourceNotFoundException("There are no reservations");
        }
        List<ReservationResponseDTO> reservationResponseDTOS = new ArrayList<>();
        for (Reservation reservation : reservations) {
            reservationResponseDTOS.add(mapper.convertValue(reservation, ReservationResponseDTO.class));
        }
        return reservationResponseDTOS;
    }

    @Override
    public void delete(Integer id) {
        Optional<Reservation> reservationOptional = reservationRepository.findById(id);
        if (reservationOptional.isPresent()) {
            reservationRepository.deleteById(id);
        } else {
            throw new ResourceNotFoundException("There is no reservation with id " + id);
        }
    }

    @Override
    public ReservationResponseDTO update(Integer id, ReservationRequestUpdateDTO reservationRequestUpDateDTO) {
        Optional<Reservation> reservationOptional = reservationRepository.findById(id);

        if (!reservationOptional.isPresent()) {
            throw new ResourceNotFoundException("La reserva con id " + id + " no fue encontrado para actualizar");
        }


        Reservation reservation = reservationOptional.get();

        if (reservationRequestUpDateDTO.getHour() != null) {
            Set<SportField> sportFieldList = sportFieldRepository.findBySport(reservation.getSportField().getSport());

            SportField sportFieldFound = null;

            for (SportField sportField : sportFieldList) {
                Boolean isAvailable = sportField.getAvailability().get(reservationRequestUpDateDTO.getHour());
                if (isAvailable != null && isAvailable) {
                    //TODO: buscar si existe reservas para esa fecha en esa cancha

                    sportFieldFound = sportField;
                    break;
                }
            }
            if (sportFieldFound == null) {
                throw new ResourceNotFoundException("There is not sport field available at the requested time. " +
                        "The reservation was not updated");
            }

            reservation.setSportField(sportFieldFound);
            reservation.setHour(reservationRequestUpDateDTO.getHour());
        }

        if (reservationRequestUpDateDTO.getPhone() != null) {
            reservation.setPhone(reservationRequestUpDateDTO.getPhone());
        }
        if (reservationRequestUpDateDTO.getDate() != null) {
            reservation.setDate(reservationRequestUpDateDTO.getDate());
        }

        Reservation reservationUpdated = reservationRepository.save(reservation);
        return mapper.convertValue(reservationUpdated, ReservationResponseDTO.class);
    }


}


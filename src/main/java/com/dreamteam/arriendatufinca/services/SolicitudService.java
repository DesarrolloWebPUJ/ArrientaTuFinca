package com.dreamteam.arriendatufinca.services;

import com.dreamteam.arriendatufinca.dtos.EstadoSolicitudDTO;
import com.dreamteam.arriendatufinca.dtos.solicitud.SimpleSolicitudDTO;
import com.dreamteam.arriendatufinca.dtos.solicitud.SolicitudDTO;
import com.dreamteam.arriendatufinca.entities.EstadoSolicitud;
import com.dreamteam.arriendatufinca.entities.Propiedad;
import com.dreamteam.arriendatufinca.entities.Solicitud;
import com.dreamteam.arriendatufinca.enums.SolicitudStatus;
import com.dreamteam.arriendatufinca.exception.ManejadorErrores;
import com.dreamteam.arriendatufinca.repository.EstadoSolicitudRepository;
import com.dreamteam.arriendatufinca.repository.PropiedadRepository;
import com.dreamteam.arriendatufinca.repository.SolicitudRepository;

import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SolicitudService {
    private final SolicitudRepository solicitudRepository;
    private final PropiedadRepository propiedadRepository;
    private final EstadoSolicitudRepository estadoSolicitudRepository;
    private final ModelMapper modelMapper;

    public SolicitudService(SolicitudRepository solicitudRepository, PropiedadRepository propiedadRepository, ModelMapper modelMapper, EstadoSolicitudRepository estadoSolicitudRepository) {
        this.solicitudRepository = solicitudRepository;
        this.propiedadRepository = propiedadRepository;
        this.modelMapper = modelMapper;
        this.estadoSolicitudRepository = estadoSolicitudRepository;
    }
    

    public List<SolicitudDTO> getAllSolicitudes() {
        List<Solicitud> solicitudes = (List<Solicitud>) solicitudRepository.findAll();
        return solicitudes.stream()
                .map(solicitud -> modelMapper.map(solicitud, SolicitudDTO.class))
                .collect(Collectors.toList());
    }

    public ResponseEntity<SimpleSolicitudDTO> getSolicitudById(Integer id) {
        Optional<Solicitud> solicitud = solicitudRepository.findById(id);
        UtilityService.verificarAusencia(solicitud, ManejadorErrores.ERROR_SOLICITUD_NO_EXISTE);

        SimpleSolicitudDTO solicitudDTO = modelMapper.map(solicitud.get(), SimpleSolicitudDTO.class);
        return ResponseEntity.ok(solicitudDTO);
    }

    public ResponseEntity<SimpleSolicitudDTO> saveSolicitud(SimpleSolicitudDTO solicitudDTO) {
        Optional<Propiedad> propiedad = propiedadRepository.findById(solicitudDTO.getPropiedad().getIdPropiedad());
        UtilityService.verificarAusencia(propiedad, ManejadorErrores.ERROR_PROPIEDAD_NO_EXISTE);
        // Verificar que la fecha inicial sea mayor a la fecha actual
        if (solicitudDTO.getFechaInicio().isBefore(LocalDateTime.now())) {
            UtilityService.devolverBadRequest(ManejadorErrores.ERROR_FECHA_INICIAL_SOLICITUD_INVALIDA);
        }
        // Verificar que la fecha final sea 1 dia mayor a la fecha de inicio
        if (!solicitudDTO.getFechaFinal().isAfter(solicitudDTO.getFechaInicio().plusDays(1))) {
            UtilityService.devolverBadRequest(ManejadorErrores.ERROR_FECHA_FINAL_SOLICITUD_INVALIDA);
        }
        if (solicitudDTO.getCantidadPersonas() <= 0 || solicitudDTO.getCantidadPersonas() > propiedad.get().getCantidadHabitaciones()) {
            UtilityService.devolverBadRequest(ManejadorErrores.ERROR_CANTIDAD_PERSONAS_SOLICITUD_INVALIDA);
        }
        Solicitud solicitud = configurarNuevaSolicitud(solicitudDTO);
        solicitud = solicitudRepository.save(solicitud);
        solicitudDTO = modelMapper.map(solicitud, SimpleSolicitudDTO.class);
    
        return ResponseEntity.ok(solicitudDTO);
    }

    private Solicitud configurarNuevaSolicitud(SimpleSolicitudDTO solicitudDTO){
        solicitudDTO.setArrendadorCalificado(false);
        solicitudDTO.setArrendatarioCalificado(false);
        solicitudDTO.setPropiedadCalificado(false);
        SolicitudStatus solicitudStatus = SolicitudStatus.PENDIENTE;
        solicitudDTO.setEstadoSolicitud(new EstadoSolicitudDTO(solicitudStatus.getId(), solicitudStatus.getNombre()));
        solicitudDTO.setFechaCreacion(LocalDateTime.now());

        Solicitud solicitud = modelMapper.map(solicitudDTO, Solicitud.class);
        solicitud.setEstadoSolicitud(estadoSolicitudRepository.findById(SolicitudStatus.PENDIENTE.getId()).get());
        return solicitud;
    }

    public ResponseEntity<SimpleSolicitudDTO> actualizarEstadoSolicitud(EstadoSolicitudDTO estadoSolicitudDTO, Integer idSolicitud){
        Optional<Solicitud> solicitudTmp = solicitudRepository.findById(idSolicitud);
        UtilityService.verificarAusencia(solicitudTmp, ManejadorErrores.ERROR_SOLICITUD_NO_EXISTE);

        Solicitud solicitud = solicitudTmp.get();
        String nuevoEstado = estadoSolicitudDTO.getNombreEstadoSolicitud();
        String viejoEstado = solicitud.getEstadoSolicitud().getNombreEstadoSolicitud();

        if (nuevoEstado != null && !nuevoEstado.equals(viejoEstado)) {
            solicitud = actualizarEstado(solicitud, nuevoEstado, viejoEstado);
        }
        
        solicitud = solicitudRepository.save(solicitud);
        SimpleSolicitudDTO solicitudDTO = modelMapper.map(solicitud, SimpleSolicitudDTO.class);

        return ResponseEntity.ok(solicitudDTO);
    }

    public ResponseEntity<SimpleSolicitudDTO> updateSolicitud(SimpleSolicitudDTO solicitudDTO) {
        ResponseEntity<SimpleSolicitudDTO> response = actualizarEstadoSolicitud(solicitudDTO.getEstadoSolicitud(), solicitudDTO.getIdSolicitud());
        
        SimpleSolicitudDTO newSolicitudDTO = response.getBody();
        newSolicitudDTO.setArrendadorCalificado(solicitudDTO.isArrendadorCalificado());
        newSolicitudDTO.setArrendatarioCalificado(solicitudDTO.isArrendatarioCalificado());
        newSolicitudDTO.setPropiedadCalificado(solicitudDTO.isPropiedadCalificado());

        Solicitud solicitud = modelMapper.map(solicitudDTO, Solicitud.class);
        solicitudRepository.save(solicitud);

        return ResponseEntity.ok(newSolicitudDTO);
    }

    private Solicitud actualizarEstado(Solicitud solicitud, String nuevoEstado, String viejoEstado){
        SolicitudStatus solicitudStatus = null;
        if (viejoEstado.equals(SolicitudStatus.PENDIENTE.getNombre()) && nuevoEstado.equals(SolicitudStatus.POR_PAGAR.getNombre())){
            solicitudStatus = SolicitudStatus.POR_PAGAR;
        }
        else if (viejoEstado.equals(SolicitudStatus.PENDIENTE.getNombre()) && nuevoEstado.equals(SolicitudStatus.RECHAZADA.getNombre())){
            solicitudStatus = SolicitudStatus.RECHAZADA;
        }
        else if (viejoEstado.equals(SolicitudStatus.POR_PAGAR.getNombre()) && nuevoEstado.equals(SolicitudStatus.POR_CALIFICAR.getNombre())){
            solicitudStatus = SolicitudStatus.POR_CALIFICAR;
        }
        else if(viejoEstado.equals(SolicitudStatus.POR_CALIFICAR.getNombre()) && nuevoEstado.equals(SolicitudStatus.CERRADA.getNombre())){
            solicitudStatus = SolicitudStatus.CERRADA;
        }
        else{
            UtilityService.devolverBadRequest(ManejadorErrores.ERROR_CAMBIO_ESTADO_INVALIDO);
        }
        Optional<EstadoSolicitud> estadoSolicitudTmp = estadoSolicitudRepository.findById(solicitudStatus.getId());
        EstadoSolicitud estadoSolicitud = estadoSolicitudTmp.get();

        solicitud.setEstadoSolicitud(estadoSolicitud);
        return solicitud;
    }

    public void deleteSolicitud(Integer id) {
        Optional<Solicitud> solicitudTmp = solicitudRepository.findById(id);
        UtilityService.verificarAusencia(solicitudTmp, ManejadorErrores.ERROR_SOLICITUD_NO_EXISTE);

        Solicitud solicitud = solicitudTmp.get();
        solicitudRepository.delete(solicitud);
    }
}

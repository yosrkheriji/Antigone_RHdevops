package com.antigone.rh.service;

import com.antigone.rh.dto.ContactClientDTO;
import com.antigone.rh.entity.ContactClient;
import com.antigone.rh.repository.ClientRepository;
import com.antigone.rh.repository.ContactClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/** Interlocuteurs additionnels chez un client (facturation, relances). */
@Service
@RequiredArgsConstructor
@Transactional
public class ContactClientService {

    private final ContactClientRepository contactClientRepository;
    private final ClientRepository clientRepository;

    public List<ContactClientDTO> getByClient(Long clientId) {
        return contactClientRepository.findByClientId(clientId).stream()
                .map(this::toDTO).collect(Collectors.toList());
    }

    public ContactClientDTO create(ContactClientDTO dto) {
        ContactClient c = ContactClient.builder()
                .client(clientRepository.findById(dto.getClientId())
                        .orElseThrow(() -> new RuntimeException("Client non trouvé")))
                .nom(dto.getNom()).poste(dto.getPoste()).email(dto.getEmail())
                .telephone(dto.getTelephone()).genre(dto.getGenre()).notes(dto.getNotes())
                .build();
        return toDTO(contactClientRepository.save(c));
    }

    public ContactClientDTO update(Long id, ContactClientDTO dto) {
        ContactClient c = contactClientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Contact non trouvé"));
        c.setNom(dto.getNom());
        c.setPoste(dto.getPoste());
        c.setEmail(dto.getEmail());
        c.setTelephone(dto.getTelephone());
        c.setGenre(dto.getGenre());
        c.setNotes(dto.getNotes());
        return toDTO(contactClientRepository.save(c));
    }

    public void delete(Long id) {
        contactClientRepository.deleteById(id);
    }

    private ContactClientDTO toDTO(ContactClient c) {
        return ContactClientDTO.builder()
                .id(c.getId()).clientId(c.getClient().getId()).nom(c.getNom()).poste(c.getPoste())
                .email(c.getEmail()).telephone(c.getTelephone()).genre(c.getGenre()).notes(c.getNotes())
                .build();
    }
}

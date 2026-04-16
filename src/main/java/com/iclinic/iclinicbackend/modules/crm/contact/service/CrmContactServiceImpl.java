package com.iclinic.iclinicbackend.modules.crm.contact.service;

import com.iclinic.iclinicbackend.modules.branch.entity.Branch;
import com.iclinic.iclinicbackend.modules.branch.repository.BranchRepository;
import com.iclinic.iclinicbackend.modules.company.entity.Company;
import com.iclinic.iclinicbackend.modules.company.repository.CompanyRepository;
import com.iclinic.iclinicbackend.modules.crm.channel.entity.ChannelConnection;
import com.iclinic.iclinicbackend.modules.crm.channel.entity.ChannelUserLink;
import com.iclinic.iclinicbackend.modules.crm.channel.repository.ChannelUserLinkRepository;
import com.iclinic.iclinicbackend.modules.crm.contact.entity.CrmContact;
import com.iclinic.iclinicbackend.modules.crm.contact.entity.CrmContactPhone;
import com.iclinic.iclinicbackend.modules.crm.contact.repository.CrmContactPhoneRepository;
import com.iclinic.iclinicbackend.modules.crm.contact.repository.CrmContactRepository;
import com.iclinic.iclinicbackend.modules.crm.exception.InvalidChannelConfigurationException;
import com.iclinic.iclinicbackend.modules.crm.webhook.dto.IncomingChannelMessage;
import com.iclinic.iclinicbackend.shared.exception.BranchNotFoundException;
import com.iclinic.iclinicbackend.shared.exception.CompanyNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class CrmContactServiceImpl implements CrmContactService {

    private final CrmContactRepository contactRepository;
    private final CrmContactPhoneRepository phoneRepository;
    private final ChannelUserLinkRepository linkRepository;
    private final CompanyRepository companyRepository;
    private final BranchRepository branchRepository;

    @Override
    public CrmContact resolveContact(ChannelConnection connection, IncomingChannelMessage msg) {
        Long companyId = connection.getCompany().getId();

        Optional<ChannelUserLink> existingLink = linkRepository
                .findByChannelTypeAndExternalUserId(msg.getChannelType(), msg.getExternalUserId());

        if (existingLink.isPresent()) {
            ChannelUserLink link = existingLink.get();
            updateLinkIfChanged(link, msg);
            log.debug("Contacto resuelto por ChannelUserLink: contactId={} channel={} externalUserId={}",
                    link.getContact().getId(), msg.getChannelType(), msg.getExternalUserId());
            return link.getContact();
        }

        if (hasPhone(msg)) {
            String normalized = normalizePhone(msg.getPhone());
            Optional<CrmContactPhone> existingPhone = phoneRepository
                    .findByNormalizedPhoneAndCompanyId(normalized, companyId);

            if (existingPhone.isPresent()) {
                CrmContact contact = existingPhone.get().getContact();
                log.info("Contacto resuelto por teléfono: contactId={} phone={}", contact.getId(), normalized);
                createChannelUserLink(contact, msg);
                return contact;
            }
        }

        log.info("Creando nuevo contacto CRM: channel={} externalUserId={} displayName={}",
                msg.getChannelType(), msg.getExternalUserId(), msg.getDisplayName());

        CrmContact contact = createNewContact(connection, msg, companyId);

        if (hasPhone(msg)) {
            createContactPhone(contact, msg.getPhone(), companyId);
        }
        createChannelUserLink(contact, msg);

        return contact;
    }

    @Override
    @Deprecated
    public CrmContact findOrCreate(Long companyId, Long branchId, String fullName, String phone) {
        if (phone != null && !phone.isBlank()) {
            Optional<CrmContact> existing = contactRepository.findByCompanyIdAndPhone(companyId, phone);
            if (existing.isPresent()) return existing.get();
        }
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new CompanyNotFoundException(companyId));
        Branch branch = resolveBranch(branchId, company);

        CrmContact contact = CrmContact.builder()
                .company(company)
                .branch(branch)
                .fullName(fullName != null && !fullName.isBlank() ? fullName : "Desconocido")
                .phone(phone)
                .sourceChannel(com.iclinic.iclinicbackend.shared.enums.ChannelType.WHATSAPP)
                .active(true)
                .build();
        return contactRepository.save(contact);
    }

    private CrmContact createNewContact(ChannelConnection connection, IncomingChannelMessage msg, Long companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new CompanyNotFoundException(companyId));

        CrmContact contact = CrmContact.builder()
                .company(company)
                .branch(connection.getBranch())
                .fullName(msg.getDisplayName())
                .phone(null)
                .sourceChannel(msg.getChannelType())
                .active(true)
                .build();
        return contactRepository.save(contact);
    }

    private void createContactPhone(CrmContact contact, String rawPhone, Long companyId) {
        String normalized = normalizePhone(rawPhone);
        if (phoneRepository.findByNormalizedPhoneAndCompanyId(normalized, companyId).isPresent()) {
            return;
        }
        CrmContactPhone cp = CrmContactPhone.builder()
                .contact(contact)
                .rawPhone(rawPhone)
                .normalizedPhone(normalized)
                .companyId(companyId)
                .build();
        phoneRepository.save(cp);
    }

    private ChannelUserLink createChannelUserLink(CrmContact contact, IncomingChannelMessage msg) {
        ChannelUserLink link = ChannelUserLink.builder()
                .contact(contact)
                .channelType(msg.getChannelType())
                .externalUserId(msg.getExternalUserId())
                .externalChatId(msg.getExternalChatId())
                .username(msg.getUsername())
                .displayName(msg.getDisplayName())
                .build();
        return linkRepository.save(link);
    }

    private void updateLinkIfChanged(ChannelUserLink link, IncomingChannelMessage msg) {
        boolean changed = false;
        if (msg.getDisplayName() != null && !msg.getDisplayName().equals(link.getDisplayName())) {
            link.setDisplayName(msg.getDisplayName());
            changed = true;
        }
        if (msg.getUsername() != null && !msg.getUsername().equals(link.getUsername())) {
            link.setUsername(msg.getUsername());
            changed = true;
        }
        if (changed) {
            linkRepository.save(link);
            log.debug("ChannelUserLink actualizado: id={}", link.getId());
        }
    }

    private Branch resolveBranch(Long branchId, Company company) {
        if (branchId == null) return null;
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new BranchNotFoundException(branchId));
        if (!branch.getCompany().getId().equals(company.getId())) {
            throw new InvalidChannelConfigurationException("La sucursal no pertenece a la empresa");
        }
        return branch;
    }

    private boolean hasPhone(IncomingChannelMessage msg) {
        return msg.getPhone() != null && !msg.getPhone().isBlank();
    }

    private String normalizePhone(String raw) {
        if (raw == null) return null;
        String cleaned = raw.replaceAll("[\\s\\-().]+", "");
        return cleaned.startsWith("+") ? cleaned : "+" + cleaned;
    }
}

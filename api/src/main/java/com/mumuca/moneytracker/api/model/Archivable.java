package com.mumuca.moneytracker.api.model;

import com.mumuca.moneytracker.api.audit.BaseAuditableEntity;
import com.mumuca.moneytracker.api.exception.ResourceAlreadyActiveException;
import com.mumuca.moneytracker.api.exception.ResourceAlreadyArchivedException;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class Archivable extends BaseAuditableEntity {

    @Column(name = "is_archived")
    private boolean isArchived = false;

    public boolean isArchived() {
        return isArchived;
    }

    protected void archive() {
        if (isArchived) {
            throw new ResourceAlreadyArchivedException(
                    "Unable to archive a resource that is already archived"
            );
        }
        this.isArchived = true;
    }

    protected void unarchive() {
        if (!isArchived) {
            throw new ResourceAlreadyActiveException(
                    "Unable to unarchive a resource that is already active"
            );
        }
        this.isArchived = false;
    }
}

package de.dk.bininja.client.controller;

import de.dk.bininja.client.model.DownloadMetadata;

/**
 * @author David Koettlitz
 * <br>Erstellt am 07.08.2017
 */
public interface ProcessorController {
   public void setDownloadTargetTo(DownloadMetadata meta);
}
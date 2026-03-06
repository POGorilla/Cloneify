/** Record pentru un playlist de pe pagina Discover (pagina default / home)
 * (id, nume, cover) afisat sub forma de card
 * @author Mirica Alin-Marian
 * @version 29 Decembrie 2025
 */

package com.proiect.spotifyclone.dto;

public record DiscoverPlaylistDTO(long id, String name, String coverUrl) {}
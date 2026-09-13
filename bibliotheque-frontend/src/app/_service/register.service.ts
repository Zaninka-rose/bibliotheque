import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export interface RegisterPayload {
  name: string;
  username: string;
  password: string;
}

/**
 * Inscription publique : endpoint backend /register (public),
 * qui force le rôle ADHERENT côté serveur.
 */
@Injectable({ providedIn: 'root' })
export class RegisterService {

  private readonly baseURL = 'http://localhost:8080/register';

  constructor(private httpClient: HttpClient) {}

  register(payload: RegisterPayload): Observable<Object> {
    return this.httpClient.post(this.baseURL, payload, {
      headers: new HttpHeaders({ 'No-Auth': 'True' })
    });
  }
}

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';

import { RegistrationComponent } from './registration.component';
import { UsersService } from '../_service/users.service';

describe('RegistrationComponent', () => {
  let component: RegistrationComponent;
  let fixture: ComponentFixture<RegistrationComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ FormsModule ],
      declarations: [ RegistrationComponent ],
      providers: [
        { provide: UsersService, useValue: { createUser: () => of({}) } },
        { provide: Router, useValue: { navigate: () => {} } }
      ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(RegistrationComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

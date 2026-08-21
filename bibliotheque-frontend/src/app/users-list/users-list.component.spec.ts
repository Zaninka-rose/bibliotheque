import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { UsersListComponent } from './users-list.component';
import { UsersService } from '../_service/users.service';
import { Router } from '@angular/router';

describe('UsersListComponent', () => {
  let component: UsersListComponent;
  let fixture: ComponentFixture<UsersListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ UsersListComponent ],
      providers: [
        { provide: UsersService, useValue: { getUsersList: () => of([]) } },
        { provide: Router, useValue: { navigate: () => {} } }
      ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(UsersListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

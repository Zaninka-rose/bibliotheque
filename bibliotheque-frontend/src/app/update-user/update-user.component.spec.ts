import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';

import { UpdateUserComponent } from './update-user.component';
import { UsersService } from '../_service/users.service';

describe('UpdateUserComponent', () => {
  let component: UpdateUserComponent;
  let fixture: ComponentFixture<UpdateUserComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ FormsModule ],
      declarations: [ UpdateUserComponent ],
      providers: [
        { provide: ActivatedRoute, useValue: { snapshot: { params: { userId: 1 } } } },
        { provide: UsersService, useValue: { getUserById: () => of({ name: '', username: '', role: [{ roleName: 'User' }] }), updateUser: () => of({}) } },
        { provide: Router, useValue: { navigate: () => {} } }
      ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(UpdateUserComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

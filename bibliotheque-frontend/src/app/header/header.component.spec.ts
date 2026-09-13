import { ComponentFixture, TestBed } from '@angular/core/testing';

import { HeaderComponent } from './header.component';
import { TPipe } from '../_service/i18n/t.pipe';
import { UsersService } from '../_service/users.service';
import { UserAuthService } from '../_service/user-auth.service';

describe('HeaderComponent', () => {
  let component: HeaderComponent;
  let fixture: ComponentFixture<HeaderComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [HeaderComponent],
      imports: [TPipe],
      providers: [
        { provide: UsersService, useValue: { roleMatch: () => false } },
        { provide: UserAuthService, useValue: { getName: () => 'Test', getToken: () => null, getRoles: () => null, isLoggedIn: () => false, clear: () => {} } }
      ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(HeaderComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { AppComponent } from './app.component';
import { HeaderComponent } from './header/header.component';
import { OnboardingComponent } from './onboarding/onboarding.component';
import { UsersService } from './_service/users.service';
import { UserAuthService } from './_service/user-auth.service';

describe('AppComponent', () => {
  let fixture: ComponentFixture<AppComponent>;
  let component: AppComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RouterTestingModule],
      declarations: [AppComponent, HeaderComponent, OnboardingComponent],
      providers: [
        { provide: UsersService, useValue: { roleMatch: () => true } },
        { provide: UserAuthService, useValue: { getUserId: () => 1 } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AppComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create the app', () => {
    expect(component).toBeTruthy();
  });

  it('affiche la bannière hors-ligne quand le navigateur est offline', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    // État initial du navigateur de test : en ligne → pas de bannière.
    expect(compiled.querySelector('.offline-banner')).toBeFalsy();
  });

  it('traite "/" comme une page sans chrome (splash plein écran)', () => {
    expect(component.isBarePage).toBe(true);
  });
});

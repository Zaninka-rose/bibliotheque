import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { HTTP_INTERCEPTORS, provideHttpClient, withInterceptorsFromDi, withXhr } from '@angular/common/http'
import { ServiceWorkerModule } from '@angular/service-worker';
import { environment } from '../environments/environment';
import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { BooksListComponent } from './books-list/books-list.component';
import { CreateBookComponent } from './create-book/create-book.component';
import { FormsModule } from '@angular/forms';
import { UpdateBookComponent } from './update-book/update-book.component';
import { BookDetailsComponent } from './book-details/book-details.component';
import { RegistrationComponent } from './registration/registration.component';
import { ReservationsComponent } from './reservations/reservations.component';
import { ReservationListComponent } from './reservation-list/reservation-list.component';
import { ReservationFormComponent } from './reservation-form/reservation-form.component';
import { UsersListComponent } from './users-list/users-list.component';
import { UserDetailsComponent } from './user-details/user-details.component';
import { UpdateUserComponent } from './update-user/update-user.component';
import { LoginComponent } from './login/login.component';
import { LogoutComponent } from './logout/logout.component';
import { HeaderComponent } from './header/header.component';
import { ForbiddenComponent } from './forbidden/forbidden.component';
import { BorrowBookComponent } from './borrow-book/borrow-book.component';
import { ReturnBookComponent } from './return-book/return-book.component';
import { OnboardingComponent } from './onboarding/onboarding.component';
import { DashboardComponent } from './dashboard/dashboard.component';
import { TPipe } from './_service/i18n/t.pipe';
import { AuthGuard } from './_auth/auth.guard';
import { AuthInterceptor } from './_auth/auth.interceptor';
import { BooksService } from './_service/books.service';
import { UsersService } from './_service/users.service';

@NgModule({ declarations: [
        AppComponent,
        BooksListComponent,
        CreateBookComponent,
        UpdateBookComponent,
        BookDetailsComponent,
        RegistrationComponent,
        ReservationsComponent,
        ReservationListComponent,
        ReservationFormComponent,
        UsersListComponent,
        UserDetailsComponent,
        UpdateUserComponent,
        LogoutComponent,
        HeaderComponent,
        ForbiddenComponent,
        BorrowBookComponent,
        ReturnBookComponent,
        OnboardingComponent,
    ],
    bootstrap: [AppComponent], imports: [BrowserModule,
        AppRoutingModule,
        FormsModule,
        TPipe,
        DashboardComponent,
        // PWA : le service worker ne s'enregistre qu'en production.
        ServiceWorkerModule.register('ngsw-worker.js', { enabled: environment.production })], providers: [
        AuthGuard,
        {
            provide: HTTP_INTERCEPTORS,
            useClass: AuthInterceptor,
            multi: true
        },
        UsersService,
        BooksService,
        provideHttpClient(withXhr(), withInterceptorsFromDi())
    ] })
export class AppModule { }

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';

import { CreateBookComponent } from './create-book.component';
import { BooksService } from '../_service/books.service';

describe('CreateBookComponent', () => {
  let component: CreateBookComponent;
  let fixture: ComponentFixture<CreateBookComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ FormsModule ],
      declarations: [ CreateBookComponent ],
      providers: [
        { provide: BooksService, useValue: { createBook: () => of({}) } },
        { provide: Router, useValue: { navigate: () => {} } }
      ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(CreateBookComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

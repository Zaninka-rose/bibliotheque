import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';

import { UpdateBookComponent } from './update-book.component';
import { BooksService } from '../_service/books.service';

describe('UpdateBookComponent', () => {
  let component: UpdateBookComponent;
  let fixture: ComponentFixture<UpdateBookComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ FormsModule ],
      declarations: [ UpdateBookComponent ],
      providers: [
        { provide: ActivatedRoute, useValue: { snapshot: { params: { bookId: 1 } } } },
        { provide: BooksService, useValue: { getBookById: () => of({}), updateBook: () => of({}) } },
        { provide: Router, useValue: { navigate: () => {} } }
      ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(UpdateBookComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

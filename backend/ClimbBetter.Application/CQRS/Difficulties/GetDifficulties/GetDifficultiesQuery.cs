using MediatR;

namespace ClimbBetter.Application.CQRS.Difficulties.GetDifficulties;

public record GetDifficultiesQuery : IRequest<List<DifficultyDto>>;
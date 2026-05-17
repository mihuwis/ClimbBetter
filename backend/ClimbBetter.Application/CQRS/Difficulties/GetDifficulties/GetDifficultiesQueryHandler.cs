using MediatR;
using ClimbBetter.Domain.Interfaces;

namespace ClimbBetter.Application.CQRS.Difficulties.GetDifficulties;

public class GetDifficultiesQueryHandler
    : IRequestHandler<GetDifficultiesQuery, List<DifficultyDto>>
{
private readonly IDifficultyRepository _difficultyRepository;

    public GetDifficultiesQueryHandler(IDifficultyRepository difficultyRepository)
    {
        _difficultyRepository = difficultyRepository;
    }

    public async Task<List<DifficultyDto>> Handle(
        GetDifficultiesQuery request,
        CancellationToken cancellationToken)
    {
        var difficulties = await _difficultyRepository.GetAllAsync(cancellationToken);
        
        return difficulties
            .OrderBy(d => d.Points)
            .Select(d => new DifficultyDto
            {
                Id = d.Id,
                Name = d.Name,
                Points = d.Points
            })
            .ToList();
    }
}